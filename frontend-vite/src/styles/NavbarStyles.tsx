import { Button } from "antd";
import { Link } from "react-router-dom";
import styled from "styled-components";

export const Nav = styled.nav`
     background-color: #1a1a1a;
     padding: 1rem 2rem;
     position: fixed;
     top: 0;
     left: 0;
     right: 0;
     z-index: 1000;
     display: flex;
     justify-content: space-between;
     align-items: center;
     box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
`;

export const Logo = styled(Link)`
     font-size: 1.5rem;
     font-weight: bold;
     color: #e50914;
     text-decoration: none;
     margin-right: 2rem;
`;

export const NavLinks = styled.div`
     display: flex;
     gap: 1.5rem;
     align-items: center;
`;

export const NavLink = styled(Link)`
     color: #ffffff;
     text-decoration: none;
     font-size: 1rem;
     transition: color 0.3s;

     &:hover {
          color: #1890ff;
     }
`;

export const SearchContainer = styled.div`
     flex: 1;
     max-width: 500px;
     margin: 0 2rem;
`;

export const AuthButton = styled(Button)`
     margin-left: 1rem;
`;
