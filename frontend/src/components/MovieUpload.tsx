import React, { useState } from 'react';
import { Form, Input, InputNumber, Upload, Button, message, Select, Card } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import styled from 'styled-components';
import api from '../services/api';

const { TextArea } = Input;

const UploadContainer = styled.div`
     max-width: 800px;
     margin: 2rem auto;
     padding: 2rem;
`;

const StyledCard = styled(Card)`
     box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
`;

interface MovieUploadForm {
     title: string;
     description: string;
     rating: number;
     categories: string[];
     poster: File | null;
}

const MovieUpload: React.FC = () => {
     const [form] = Form.useForm();
     const [loading, setLoading] = useState(false);
     const [imageUrl, setImageUrl] = useState<string>();

     const onFinish = async (values: MovieUploadForm) => {
          try {
               setLoading(true);
               const formData = new FormData();
               formData.append('title', values.title);
               formData.append('description', values.description);
               formData.append('rating', values.rating.toString());
               values.categories.forEach(category => 
                    formData.append('categories', category)
               );
               if (values.poster) {
                    formData.append('poster', values.poster);
               }

               await api.post('/movies', formData, {
                    headers: {
                         'Content-Type': 'multipart/form-data',
                    }
               });

               message.success('Movie created successfully!');
               form.resetFields();
               setImageUrl(undefined);
          } catch (error) {
               message.error('Failed to create movie');
               console.error('Error creating movie:', error);
          } finally {
               setLoading(false);
          }
     };

     const beforeUpload = (file: File) => {
          const isImage = file.type.startsWith('image/');
          if (!isImage) {
               message.error('You can only upload image files!');
               return false;
          }
          return false;
     };

     const handleChange = (info: any) => {
          if (info.file) {
               const reader = new FileReader();
               reader.onload = (e) => {
                    setImageUrl(e.target?.result as string);
               };
               reader.readAsDataURL(info.file);
          }
     };

     return (
          <UploadContainer>
               <StyledCard title="Upload New Movie">
                    <Form
                         form={form}
                         layout="vertical"
                         onFinish={onFinish}
                    >
                    <Form.Item
                         name="title"
                         label="Movie Title"
                         rules={[{ required: true, message: 'Please input the movie title!' }]}
                         >
                         <Input />
                    </Form.Item>

                    <Form.Item
                         name="description"
                         label="Description"
                         rules={[{ required: true, message: 'Please input the movie description!' }]}
                    >
                         <TextArea rows={4} />
                    </Form.Item>

                    <Form.Item
                         name="rating"
                         label="Rating"
                         rules={[{ required: true, message: 'Please input the rating!' }]}
                    >
                         <InputNumber min={0} max={10} step={0.1} style={{ width: '100%' }} />
                    </Form.Item>

                    <Form.Item
                         name="categories"
                         label="Categories"
                         rules={[{ required: true, message: 'Please select at least one category!' }]}
                    >
                         <Select mode="multiple" placeholder="Select categories">
                              <Select.Option value="action">Action</Select.Option>
                              <Select.Option value="comedy">Comedy</Select.Option>
                              <Select.Option value="drama">Drama</Select.Option>
                              {/* Add more categories as needed */}
                         </Select>
                    </Form.Item>

                    <Form.Item
                         name="poster"
                         label="Movie Poster"
                         rules={[{ required: true, message: 'Please upload a movie poster!' }]}
                    >
                         <Upload
                              name="poster"
                              listType="picture-card"
                              showUploadList={false}
                              beforeUpload={beforeUpload}
                              onChange={handleChange}
                         >
                              {imageUrl ? (
                                   <img src={imageUrl} alt="poster" style={{ width: '100%' }} />
                              ) : (
                                   <div>
                                        <UploadOutlined />
                                        <div style={{ marginTop: 8 }}>Upload</div>
                                   </div>
                              )}
                         </Upload>
                    </Form.Item>

                    <Form.Item>
                         <Button type="primary" htmlType="submit" loading={loading} block>
                              Upload Movie
                         </Button>
                    </Form.Item>
                    </Form>
               </StyledCard>
          </UploadContainer>
     );
};

export default MovieUpload;