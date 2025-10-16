// frontend/src/pages/Watchlist.tsx
import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Play, Plus, Edit, Trash2 } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { ImageWithFallback } from '../components/ImageWithFallback';
import { message, Modal, Form, Input, Select } from 'antd';
import watchlistService, {
    CreateCollectionRequest,
    Page,
    UpdateCollectionRequest,
    UpsertItemRequest,
    WatchlistCollection,
    WatchlistItem
} from '../services/watchlistService';

type CollectionsState = Page<WatchlistCollection> & { loading: boolean };

type ItemsState = {
    response: Page<WatchlistItem> | null;
    loading: boolean;
};

const Watchlist: React.FC = () => {
    const [collections, setCollections] = useState<CollectionsState>({
        content: [],
        page: 0,
        size: 20,
        totalPages: 0,
        totalItems: 0,
        hasNext: false,
        hasPrevious: false,
        loading: false
    });
    const [selectedCollection, setSelectedCollection] = useState<WatchlistCollection | null>(null);
    const [items, setItems] = useState<ItemsState>({ response: null, loading: false });
    const [collectionForm] = Form.useForm<CreateCollectionRequest | UpdateCollectionRequest>();
    const [itemForm] = Form.useForm<UpsertItemRequest>();
    const [isCollectionModalVisible, setCollectionModalVisible] = useState(false);
    const [isItemModalVisible, setItemModalVisible] = useState(false);
    const [editingCollection, setEditingCollection] = useState<WatchlistCollection | null>(null);
    const [editingItem, setEditingItem] = useState<WatchlistItem | null>(null);

    const statusColorMap: Record<string, string> = useMemo(() => ({
        WATCHING: 'blue',
        COMPLETED: 'green',
        PLAN_TO_WATCH: 'gold',
        DROPPED: 'red'
    }), []);

    const fetchCollections = async (page = 0, size = 20) => {
        try {
            setCollections(prev => ({ ...prev, loading: true }));
            const response = await watchlistService.listCollections(page, size);
            setCollections({ ...response, loading: false });

            if (response.content.length === 0) {
                setSelectedCollection(null);
                setItems({ response: null, loading: false });
                return;
            }

            const existingSelection = response.content.find(c => c.id === selectedCollection?.id);
            setSelectedCollection(existingSelection ?? response.content[0]);
        } catch (error) {
            console.error(error);
            message.error('Failed to load watchlist collections');
            setCollections(prev => ({ ...prev, loading: false }));
        }
    };

    const fetchItems = async (collection: WatchlistCollection, page = 0, size = 20) => {
        try {
            setItems({ response: null, loading: true });
            const response = await watchlistService.listItems(collection.id, page, size);
            setItems({ response, loading: false });
        } catch (error) {
            console.error(error);
            message.error('Failed to load movies in this collection');
            setItems({ response: null, loading: false });
        }
    };

    useEffect(() => {
        fetchCollections();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    useEffect(() => {
        if (selectedCollection) {
            fetchItems(selectedCollection);
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedCollection?.id]);

    const openCreateCollectionModal = () => {
        setEditingCollection(null);
        collectionForm.resetFields();
        setCollectionModalVisible(true);
    };

    const openEditCollectionModal = (collection: WatchlistCollection) => {
        setEditingCollection(collection);
        collectionForm.setFieldsValue({
            name: collection.name,
            description: collection.description,
            isPublic: collection.isPublic
        });
        setCollectionModalVisible(true);
    };

    const handleSaveCollection = async (values: CreateCollectionRequest | UpdateCollectionRequest) => {
        try {
            if (editingCollection) {
                const updated = await watchlistService.updateCollection(editingCollection.id, values);
                setSelectedCollection(updated);
                message.success('Collection updated');
            } else {
                const created = await watchlistService.createCollection(values);
                setSelectedCollection(created);
                message.success('Collection created');
            }
            setCollectionModalVisible(false);
            fetchCollections(collections.page, collections.size);
        } catch (error) {
            console.error(error);
            message.error('Failed to save collection');
        }
    };

    const handleDeleteCollection = (collection: WatchlistCollection) => {
        Modal.confirm({
            title: 'Delete collection',
            content: `Are you sure you want to delete "${collection.name}"?`,
            okType: 'danger',
            onOk: async () => {
                try {
                    await watchlistService.deleteCollection(collection.id);
                    message.success('Collection deleted');
                    if (collection.id === selectedCollection?.id) {
                        setSelectedCollection(null);
                    }
                    fetchCollections(collections.page, collections.size);
                } catch (error) {
                    console.error(error);
                    message.error('Failed to delete collection');
                }
            }
        });
    };

    const openCreateItemModal = () => {
        if (!selectedCollection) return;
        setEditingItem(null);
        itemForm.resetFields();
        setItemModalVisible(true);
    };

    const openEditItemModal = (item: WatchlistItem) => {
        setEditingItem(item);
        itemForm.setFieldsValue({
            movieId: item.movieId,
            status: item.status,
            notes: item.notes
        });
        setItemModalVisible(true);
    };

    const handleSaveItem = async (values: UpsertItemRequest) => {
        if (!selectedCollection) return;
        try {
            if (editingItem) {
                await watchlistService.updateItem(selectedCollection.id, editingItem.movieId, values);
                message.success('Movie updated');
            } else {
                await watchlistService.addItem(selectedCollection.id, values);
                message.success('Movie added');
            }
            setItemModalVisible(false);
            fetchItems(selectedCollection);
        } catch (error) {
            console.error(error);
            message.error('Failed to save movie');
        }
    };

    const handleDeleteItem = (item: WatchlistItem) => {
        if (!selectedCollection) return;
        Modal.confirm({
            title: 'Remove movie',
            content: 'Are you sure you want to remove this movie from the collection?',
            okType: 'danger',
            onOk: async () => {
                try {
                    await watchlistService.deleteItem(selectedCollection.id, item.movieId);
                    message.success('Movie removed');
                    fetchItems(selectedCollection);
                } catch (error) {
                    console.error(error);
                    message.error('Failed to remove movie');
                }
            }
        });
    };


    const navigate = useNavigate();

    return (
        <div className="min-h-screen bg-black pt-20 pb-12 px-4 md:px-12">
            <div className="max-w-7xl mx-auto space-y-8">
                {/* Header */}
                <div className="flex items-center justify-between">
                    <div>
                        <h1 className="text-white text-4xl font-bold">My Watchlist</h1>
                        <p className="text-gray-400 mt-2">
                            Organize your favourite movies into collections and track your progress
                        </p>
                    </div>
                    <Button onClick={openCreateCollectionModal} className="bg-red-600 hover:bg-red-700 text-white gap-2">
                        <Plus className="h-4 w-4" />
                        New Collection
                    </Button>
                </div>

                <div className="grid md:grid-cols-4 gap-6">
                    {/* Collections Sidebar */}
                    <div className="md:col-span-1">
                        <Card className="bg-gray-900 border-gray-800">
                            <CardHeader>
                                <CardTitle className="text-white">Collections</CardTitle>
                            </CardHeader>
                            <CardContent className="space-y-2">
                                {collections.loading ? (
                                    <div className="text-center text-gray-400">Loading...</div>
                                ) : collections.content.length === 0 ? (
                                    <div className="text-center text-gray-400 py-4">No collections yet</div>
                                ) : (
                                    collections.content.map((collection) => (
                                        <div
                                            key={collection.id}
                                            onClick={() => setSelectedCollection(collection)}
                                            className={`p-3 rounded-md cursor-pointer transition-colors ${
                                                selectedCollection?.id === collection.id
                                                    ? 'bg-red-600 text-white'
                                                    : 'bg-gray-800 text-gray-300 hover:bg-gray-700'
                                            }`}
                                        >
                                            <div className="flex items-center justify-between">
                                                <span className="font-medium truncate">{collection.name}</span>
                                                <div className="flex gap-1">
                                                    <button
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            openEditCollectionModal(collection);
                                                        }}
                                                        className="p-1 hover:bg-gray-600 rounded"
                                                    >
                                                        <Edit className="h-3 w-3" />
                                                    </button>
                                                    <button
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            handleDeleteCollection(collection);
                                                        }}
                                                        className="p-1 hover:bg-gray-600 rounded"
                                                    >
                                                        <Trash2 className="h-3 w-3" />
                                                    </button>
                                                </div>
                                            </div>
                                            {collection.description && (
                                                <p className="text-xs mt-1 truncate opacity-75">{collection.description}</p>
                                            )}
                                        </div>
                                    ))
                                )}
                            </CardContent>
                        </Card>
                    </div>

                    {/* Movies Grid */}
                    <div className="md:col-span-3">
                        <Card className="bg-gray-900 border-gray-800">
                            <CardHeader className="flex flex-row items-center justify-between">
                                <div>
                                    <CardTitle className="text-white">
                                        {selectedCollection ? selectedCollection.name : 'Select a collection'}
                                    </CardTitle>
                                    {selectedCollection?.description && (
                                        <CardDescription>{selectedCollection.description}</CardDescription>
                                    )}
                                </div>
                                {selectedCollection && (
                                    <Button onClick={openCreateItemModal} className="bg-red-600 hover:bg-red-700 text-white">
                                        Add Movie
                                    </Button>
                                )}
                            </CardHeader>
                            <CardContent>
                                {!selectedCollection ? (
                                    <div className="text-center text-gray-400 py-12">
                                        Select a collection to view movies
                                    </div>
                                ) : items.loading ? (
                                    <div className="text-center text-gray-400 py-12">Loading movies...</div>
                                ) : !items.response || items.response.content.length === 0 ? (
                                    <div className="text-center text-gray-400 py-12">
                                        No movies in this collection yet
                                    </div>
                                ) : (
                                    <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
                                        {items.response.content.map((item) => (
                                            <div key={item.id} className="group relative">
                                                <div className="aspect-[2/3] bg-gray-800 rounded-md overflow-hidden">
                                                    <div className="w-full h-full flex items-center justify-center text-gray-500">
                                                        Movie {item.movieId}
                                                    </div>
                                                </div>
                                                <div className="mt-2 space-y-1">
                                                    <Badge className={`${
                                                        item.status === 'COMPLETED' ? 'bg-green-600' :
                                                        item.status === 'WATCHING' ? 'bg-blue-600' :
                                                        item.status === 'PLAN_TO_WATCH' ? 'bg-yellow-600' :
                                                        'bg-red-600'
                                                    } text-white text-xs`}>
                                                        {item.status.replace('_', ' ')}
                                                    </Badge>
                                                    <div className="flex gap-1 mt-2">
                                                        <button
                                                            onClick={() => openEditItemModal(item)}
                                                            className="flex-1 bg-gray-800 hover:bg-gray-700 text-white text-xs py-1 px-2 rounded"
                                                        >
                                                            Edit
                                                        </button>
                                                        <button
                                                            onClick={() => handleDeleteItem(item)}
                                                            className="flex-1 bg-gray-800 hover:bg-gray-700 text-white text-xs py-1 px-2 rounded"
                                                        >
                                                            Remove
                                                        </button>
                                                    </div>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </CardContent>
                        </Card>
                    </div>
                </div>
            </div>

            <Modal
                title={editingCollection ? 'Edit collection' : 'New collection'}
                open={isCollectionModalVisible}
                onCancel={() => setCollectionModalVisible(false)}
                onOk={() => collectionForm.submit()}
                okText="Save"
            >
                <Form
                    layout="vertical"
                    form={collectionForm}
                    onFinish={handleSaveCollection}
                    initialValues={{ isPublic: true }}
                >
                    <Form.Item
                        label="Name"
                        name="name"
                        rules={[{ required: true, message: 'Please enter a collection name' }]}
                    >
                        <Input placeholder="My sci-fi favourites" />
                    </Form.Item>
                    <Form.Item label="Description" name="description">
                        <Input.TextArea rows={3} placeholder="Optional description" />
                    </Form.Item>
                    <Form.Item
                        label="Visibility"
                        name="isPublic"
                        rules={[{ required: true, message: 'Please choose visibility' }]}
                    >
                        <Select
                            options={[
                                { label: 'Public', value: true },
                                { label: 'Private', value: false }
                            ]}
                        />
                    </Form.Item>
                </Form>
            </Modal>

            <Modal
                title={editingItem ? 'Edit movie' : 'Add movie'}
                open={isItemModalVisible}
                onCancel={() => setItemModalVisible(false)}
                onOk={() => itemForm.submit()}
                okText="Save"
            >
                <Form form={itemForm} layout="vertical" onFinish={handleSaveItem}>
                    <Form.Item
                        label="Movie ID"
                        name="movieId"
                        rules={[{ required: true, message: 'Movie ID is required' }]}
                    >
                        <Input placeholder="Movie identifier" disabled={!!editingItem} />
                    </Form.Item>
                    <Form.Item
                        label="Status"
                        name="status"
                        rules={[{ required: true, message: 'Please select a status' }]}
                    >
                        <Select
                            options={[
                                { label: 'Watching', value: 'WATCHING' },
                                { label: 'Completed', value: 'COMPLETED' },
                                { label: 'Plan to Watch', value: 'PLAN_TO_WATCH' },
                                { label: 'Dropped', value: 'DROPPED' }
                            ]}
                        />
                    </Form.Item>
                    <Form.Item label="Notes" name="notes">
                        <Input.TextArea rows={3} placeholder="Optional notes" />
                    </Form.Item>
                </Form>
            </Modal>
        </div>
    );
};

export default Watchlist;