import React, { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import SearchBar from './SearchBar';
import { Logo, Nav, NavLinks, SearchContainer } from '../styles/NavbarStyles';
import { useSelector } from 'react-redux';
import { RootState } from '../redux/store';
import UserMenu from './UserMenu';
import AuthLinks from './AuthLinks';

const Navbar: React.FC = () => {
  const navigate = useNavigate();
  const [searchLoading, setSearchLoading] = useState(false);

  const user = useSelector((state: RootState) => state.auth.user);
  const isAuthenticated = !!useSelector((state: RootState) => state.auth.auth?.accessToken);

  const handleSearch = (query: string, limit: number = 10) => {
    if (!query.trim()) return;

    setSearchLoading(true);
    navigate(`/search?query=${encodeURIComponent(query)}&limit=${limit}`);
    setSearchLoading(false);
  };

  return (
    <Nav>
      <Logo to="/">BBMovie</Logo>

      <SearchContainer>
        <SearchBar
          placeholder="Search movies, categories..."
          onSearch={handleSearch}
          loading={searchLoading}
        />
      </SearchContainer>

      <NavLinks>
        <NavLink to="/movies">Movies</NavLink>
        <NavLink to="/categories">Categories</NavLink>

        {isAuthenticated ? (
          <UserMenu userEmail={user?.email} />
        ) : (
          <AuthLinks />
        )}
      </NavLinks>
    </Nav>
  );
};

export default Navbar;